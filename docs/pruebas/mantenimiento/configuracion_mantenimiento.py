"""Rutas locales configurables para las pruebas del equipo."""
from pathlib import Path
import os,shutil

def raiz_proyecto(script):
    if os.getenv('TRANSADUANAS_PROYECTO'):return Path(os.environ['TRANSADUANAS_PROYECTO']).expanduser().resolve()
    for folder in Path(script).resolve().parents:
        if (folder/'backend-sga/pom.xml').is_file():return folder
    raise RuntimeError('No se encontro la raiz del proyecto; definir TRANSADUANAS_PROYECTO.')
def ruta_despliegue(project):return Path(os.environ.get('TRANSADUANAS_DESPLIEGUE',str(Path(project).parent/'TransAduanas-desplegado'))).expanduser().resolve()
def ruta_herramientas(project):return Path(os.environ.get('TRANSADUANAS_HERRAMIENTAS',str(Path(project).parent/'TransAduanas-herramientas'))).expanduser().resolve()
def ruta_laboratorio(project):return Path(os.environ.get('TRANSADUANAS_LABORATORIOS',str(Path(project).parent/'TransAduanas-laboratorios'))).expanduser().resolve()
def evidencias_originales():return Path(os.environ.get('TRANSADUANAS_EVIDENCIAS_ORIGINALES',str(Path.home()/'TransAduanas-evidencias'))).expanduser().resolve()
def mysql_home():
    if os.getenv('MYSQL_HOME'):return Path(os.environ['MYSQL_HOME']).expanduser().resolve()
    executable=shutil.which('mysql.exe')
    return Path(executable).resolve().parent.parent if executable else Path(os.environ.get('ProgramFiles',r'C:\Program Files'))/'MySQL/MySQL Server 8.4'
def java_home():
    if os.getenv('JAVA_HOME'):return Path(os.environ['JAVA_HOME']).expanduser().resolve()
    executable=shutil.which('javac.exe') or shutil.which('java.exe')
    return Path(executable).resolve().parent.parent if executable else Path(os.environ.get('ProgramFiles',r'C:\Program Files'))/'Eclipse Adoptium/jdk-21.0.13.9-hotspot'
def powershell():return Path(os.environ.get('WINDIR',r'C:\Windows'))/'System32/WindowsPowerShell/v1.0/powershell.exe'
def programa_git():
    executable=os.getenv('GIT_EXE') or shutil.which('git.exe') or shutil.which('git')
    if not executable:raise RuntimeError('Git no disponible en PATH; definir GIT_EXE.')
    return executable

def programa_maven():
    executable=os.getenv('MAVEN_CMD') or shutil.which('mvn.cmd')
    if executable:return executable
    found=sorted((Path.home()/'.m2/wrapper/dists').glob('apache-maven-*/**/bin/mvn.cmd'))
    if not found:raise RuntimeError('Maven no disponible. Definir MAVEN_CMD o preparar el wrapper.')
    return str(found[-1])
