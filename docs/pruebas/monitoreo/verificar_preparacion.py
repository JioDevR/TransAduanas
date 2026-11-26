"""Comprobaciones aisladas del ejecutor; no son ejecuciones M01 a M15."""
from datetime import datetime,timezone
from pathlib import Path
from types import SimpleNamespace
import ast
import io
import json
import math
import tempfile
import unittest
import urllib.parse
import urllib.request
from contextlib import redirect_stdout
from leer_almacenamiento_backend import leer_almacenamiento_backend

ROOT=Path(__file__).parent


class PreparacionCPU(unittest.TestCase):
    def check_gate(self,code,value,connection_errors=0):
        text=(ROOT/f'ejecutar_prueba_{code[1:]}.py').read_text(encoding='utf-8-sig')
        start=text.index('cpu_preparacion=[]')
        end=text.index('initial=windows()',start) if code=='M09' else text.index('live=[',start)
        with tempfile.TemporaryDirectory() as directory:
            raw='' if value is None else '\n'.join(f'{metric}{{a="b"}} {value}' for metric in ('system_cpu_usage','process_cpu_usage'))
            calls={'count':0}
            def native_cpu():
                calls['count']+=1
                if connection_errors==-1 or calls['count']<=connection_errors:
                    raise OSError('Conexion rechazada en comprobacion aislada')
                return 0,raw
            env={'out':Path(directory),'json':json,'math':math,
                 'time':SimpleNamespace(time=lambda:1000,sleep=lambda _:None),
                 'marker':'revision-local','created':datetime.now(timezone.utc),
                 'handle':None,'number':lambda expr:value,'native_cpu':native_cpu,
                 'expressions':{'CPUHOST':'host','CPUJAVA':'java','UP':'up'},
                 'prom':lambda expr,when:1 if expr=='up' else None if value is None else value*100}
            stopped=False
            for node in ast.parse(text).body:
                if isinstance(node,ast.FunctionDef) and node.name=='preparacion_no_disponible':
                    exec(compile(ast.Module(body=[node],type_ignores=[]),'preparacion-helper','exec'),env)
            with redirect_stdout(io.StringIO()):
                try:exec(compile(text[start:end],f'{code}-preparacion','exec'),env)
                except SystemExit:stopped=True
            self.assertEqual(stopped,value is None or value<0 or connection_errors==-1)
            if stopped:
                result=json.loads((Path(directory)/'resultado.json').read_text(encoding='utf-8'))
                self.assertEqual(result['Estado'],'NO_EJECUTADA')

    def test_m09_valid(self):self.check_gate('M09',.15)
    def test_m09_unavailable(self):self.check_gate('M09',-1)
    def test_m09_missing(self):self.check_gate('M09',None)
    def test_m09_connection_recovers(self):self.check_gate('M09',.15,connection_errors=2)
    def test_m09_connection_stays_unavailable(self):self.check_gate('M09',.15,connection_errors=-1)
    def test_m14_valid(self):self.check_gate('M14',.15)
    def test_m14_unavailable(self):self.check_gate('M14',-1)
    def test_m14_missing(self):self.check_gate('M14',None)


class DisponibilidadM09(unittest.TestCase):
    def check_startup(self,delayed_metrics=False,unavailable=False):
        source=(ROOT/'ejecutar_prueba_09.py').read_text(encoding='utf-8-sig')
        start=source.index('urls_preparacion=[')
        end=source.index('baseline=powershell(',start)
        clock={'value':0};cycles={'count':0}
        def sleep(seconds):clock['value']+=seconds
        def open_url(url,timeout):
            if ':8086/' in url:cycles['count']+=1
            if ':9086/' in url and (unavailable or delayed_metrics and cycles['count']<=2):
                raise OSError('Puerto de metricas no disponible en comprobacion aislada')
            response=io.BytesIO(b'{}');response.status=200
            return response
        with tempfile.TemporaryDirectory() as directory:
            env={'out':Path(directory),'marker':'revision-inicio','created':datetime.now(timezone.utc),
                 'json':json,'handle':None,'time':SimpleNamespace(monotonic=lambda:clock['value'],sleep=sleep),
                 'urllib':SimpleNamespace(request=SimpleNamespace(urlopen=open_url))}
            for node in ast.parse(source).body:
                if isinstance(node,ast.FunctionDef) and node.name=='preparacion_no_disponible':
                    exec(compile(ast.Module(body=[node],type_ignores=[]),'preparacion-helper','exec'),env)
            stopped=False
            with redirect_stdout(io.StringIO()):
                try:exec(compile(source[start:end],'M09-disponibilidad','exec'),env)
                except SystemExit as exc:
                    stopped=True
                    self.assertEqual(exc.code,3)
            self.assertEqual(stopped,unavailable)
            self.assertTrue((Path(directory)/'preparacion-disponibilidad.json').is_file())
            if unavailable:
                result=json.loads((Path(directory)/'resultado.json').read_text(encoding='utf-8'))
                self.assertEqual(result['Estado'],'NO_EJECUTADA')
                self.assertEqual(result['SolicitudesMedidas'],0)
            elif delayed_metrics:
                self.assertEqual(cycles['count'],3)

    def test_ready(self):self.check_startup()
    def test_metrics_start_later(self):self.check_startup(delayed_metrics=True)
    def test_metrics_never_start(self):self.check_startup(unavailable=True)


class PreparacionM04(unittest.TestCase):
    def run_gate(self,initial,lag=False):
        source=(ROOT/'ejecutar_prueba_04_prometheus.py').read_text(encoding='utf-8-sig')
        start=source.index('# Preparacion fuera de las 60 consultas medidas.')
        end=source.index('initial_offset =',start)
        counter={'value':initial};requests=[];clock={'value':0}
        endpoint='/api/v1/catalogos/tipos-carga';marker='M04-identificador-medicion'

        def raw():
            return counter['value'],('http_server_requests_seconds_count{uri="'+endpoint+'",method="GET",status="200"} '+str(counter['value'])) if counter['value'] else ''

        def open_request(request,timeout):
            requests.append(request.full_url)
            counter['value']+=1
            response=io.BytesIO(b'{"success":true}')
            response.status=200
            return response

        def sleep(seconds):clock['value']+=seconds

        with tempfile.TemporaryDirectory() as directory:
            env={'out':Path(directory),'marker':marker,'endpoint':endpoint,'json':json,
                 'created':datetime.now(timezone.utc),'raw_metrics':raw,
                 'time':SimpleNamespace(monotonic=lambda:clock['value'],sleep=sleep),
                 'urllib':SimpleNamespace(parse=urllib.parse,request=SimpleNamespace(Request=urllib.request.Request,urlopen=open_request)),
                 'counter_expr':'counter','prom':lambda expr:1 if expr.startswith('up{') else counter['value']-1 if lag else counter['value'],
                 'grafana_values':lambda:({'CONTADOR':counter['value']},{})}
            stopped=False
            with redirect_stdout(io.StringIO()):
                try:exec(compile(source[start:end],'M04-preparacion','exec'),env)
                except SystemExit:stopped=True
            self.assertEqual(stopped,lag)
            self.assertEqual(len(requests),1 if initial==0 else 0)
            for url in requests:
                self.assertNotIn(marker,url)
                self.assertIn('PREPARACION-M04-',url)
            if stopped:
                result=json.loads((Path(directory)/'resultado.json').read_text(encoding='utf-8'))
                self.assertEqual(result['Estado'],'NO_EJECUTADA')
                self.assertEqual(result['SolicitudesMedidas'],0)
            else:
                self.assertEqual(env['native_before'],initial or 1)
                self.assertEqual(env['prom_before'],env['graf_before']['CONTADOR'])

    def test_cold_start_has_separate_preparation(self):self.run_gate(0)
    def test_existing_counter_needs_no_preparation_request(self):self.run_gate(7)
    def test_unsynchronized_counter_does_not_start(self):self.run_gate(7,lag=True)


class AlmacenamientoAnterior(unittest.TestCase):
    def test_winsw_storage(self):
        with tempfile.TemporaryDirectory() as directory:
            storage=Path(directory)/'documentos';storage.mkdir()
            xml=Path(directory)/'service.xml'
            xml.write_text(f'<service><env name="STORAGE_DIR" value="{storage}"/></service>',encoding='utf-8')
            self.assertEqual(leer_almacenamiento_backend(xml),(storage.resolve(),'WinSW'))


if __name__=='__main__':
    for path in ROOT.glob('*.py'):
        ast.parse(path.read_text(encoding='utf-8-sig'),filename=path.name)
    unittest.main(verbosity=2)
