#!/usr/bin/env python3
"""Lanzador interactivo y autodetectable para utilidades de monitoreo.

Detecta automáticamente el sistema operativo (Linux o Windows), escanea las
carpetas correspondientes en ``utilities/monitoreo/`` y permite seleccionar y
ejecutar cualquier utilidad sin mantener listas fijas.
"""

from __future__ import annotations

import argparse
import os
import platform
import re
import shlex
import shutil
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


BASE_DIR = Path(__file__).resolve().parent
LINUX_DIR = BASE_DIR / "scripts" / "linux"
WIN_DIR = BASE_DIR / "scripts" / "win"

EXCLUDED_NAMES = {"monitoreo.py"}
DESC_PATTERN = re.compile(r"^\s*#\s*(?:DESC|DESCRIPCION|DESCRIPCIÓN)\s*:\s*(.+?)\s*$", re.IGNORECASE)


@dataclass(frozen=True)
class UtilityScript:
    """Representa una utilidad de monitoreo detectada en disco."""

    name: str
    title: str
    path: Path
    kind: str
    category: str
    description: str


def supports_ansi() -> bool:
    """Determina si la terminal soporta colores ANSI y los habilita en Windows."""
    if not sys.stdout.isatty():
        return False
    if os.name == "nt":
        try:
            import ctypes

            kernel32 = ctypes.windll.kernel32  # type: ignore[attr-defined]
            handle = kernel32.GetStdHandle(-11)
            mode = ctypes.c_ulong()
            if kernel32.GetConsoleMode(handle, ctypes.byref(mode)):
                kernel32.SetConsoleMode(handle, mode.value | 0x0004)
                return True
        except Exception:
            return False
    return True


USE_COLOR = supports_ansi()


def style(text: str, code: str) -> str:
    if not USE_COLOR:
        return text
    return f"\033[{code}m{text}\033[0m"


def detect_platform(override: str | None = None) -> str:
    """Devuelve 'linux', 'win' o 'all' según el SO o el override indicado."""
    if override in {"linux", "win", "all"}:
        return override
    sys_name = platform.system().lower()
    if sys_name.startswith("win"):
        return "win"
    return "linux"


def format_title(stem: str) -> str:
    """Convierte un nombre de archivo en un título legible."""
    cleaned = re.sub(r"[-_]+", " ", stem).strip()
    return " ".join(word.capitalize() if word.islower() else word for word in cleaned.split())


def extract_description(path: Path) -> str:
    """Lee las primeras líneas buscando '# DESC: ...' como descripción opcional."""
    try:
        with path.open("r", encoding="utf-8", errors="replace") as fh:
            for idx, line in enumerate(fh):
                if idx >= 20:
                    break
                match = DESC_PATTERN.match(line)
                if match:
                    return match.group(1)
    except OSError:
        pass
    return ""


def is_ignored_file(path: Path) -> bool:
    """Filtra archivos internos (prefijo '_' o '.') y el propio lanzador."""
    name = path.name
    return (
        not path.is_file()
        or name in EXCLUDED_NAMES
        or name.startswith("_")
        or name.startswith(".")
    )


def scan_directory(directory: Path, extensions: set[str], category: str) -> list[UtilityScript]:
    """Escanea una carpeta en busca de scripts ejecutables según extensión."""
    if not directory.is_dir():
        return []

    found: list[UtilityScript] = []
    for entry in sorted(directory.iterdir(), key=lambda p: p.name.lower()):
        if is_ignored_file(entry):
            continue
        ext = entry.suffix.lower()
        if ext not in extensions:
            continue
        found.append(
            UtilityScript(
                name=entry.name,
                title=format_title(entry.stem),
                path=entry,
                kind=ext.lstrip("."),
                category=category,
                description=extract_description(entry),
            )
        )
    return found


def discover_utilities(target_os: str) -> list[UtilityScript]:
    """Descubre automáticamente todas las utilidades aplicables a la plataforma."""
    items: list[UtilityScript] = []

    if target_os in {"linux", "all"}:
        items.extend(scan_directory(LINUX_DIR, {".sh", ".py"}, "Linux"))
    if target_os in {"win", "all"}:
        items.extend(scan_directory(WIN_DIR, {".ps1", ".py", ".bat", ".cmd"}, "Windows"))

    items.extend(scan_directory(BASE_DIR, {".py"}, "General (Python)"))
    return items


def resolve_powershell() -> str:
    """Obtiene el binario de PowerShell disponible en el sistema."""
    for candidate in ("pwsh", "powershell.exe", "powershell"):
        found = shutil.which(candidate)
        if found:
            return found
    return "powershell.exe"


def build_command(script: UtilityScript, extra_args: list[str]) -> list[str]:
    """Construye el comando de ejecución según la extensión del script."""
    ext = script.path.suffix.lower()
    if ext == ".sh":
        return ["bash", str(script.path), *extra_args]
    if ext == ".ps1":
        return [
            resolve_powershell(),
            "-NoProfile",
            "-ExecutionPolicy",
            "Bypass",
            "-File",
            str(script.path),
            *extra_args,
        ]
    if ext == ".py":
        return [sys.executable, str(script.path), *extra_args]
    if ext in {".bat", ".cmd"}:
        return ["cmd.exe", "/c", str(script.path), *extra_args]
    return [str(script.path), *extra_args]


def run_utility(script: UtilityScript, extra_args: list[str]) -> int:
    """Ejecuta la utilidad seleccionada mostrando el comando lanzado."""
    cmd = build_command(script, extra_args)
    print()
    print(style(f"==> Ejecutando: {' '.join(cmd)}", "1;36"))
    print("-" * 60)
    try:
        completed = subprocess.run(cmd, cwd=str(BASE_DIR))
        code = completed.returncode
    except KeyboardInterrupt:
        print(style("\nEjecución interrumpida por el usuario.", "33"))
        code = 130
    except FileNotFoundError as exc:
        print(style(f"No se pudo iniciar el intérprete requerido: {exc}", "31"), file=sys.stderr)
        code = 127
    print("-" * 60)
    status_color = "32" if code == 0 else "31"
    print(style(f"Finalizado con código de salida: {code}", status_color))
    return code


def print_menu(utilities: list[UtilityScript], target_os: str) -> None:
    """Muestra el selector numerado agrupado por categoría."""
    os_label = {"linux": "Linux", "win": "Windows", "all": "Todas las plataformas"}[target_os]
    print()
    print(style("=== Utilidades de Monitoreo TransAduanas ===", "1;34"))
    print(f"Plataforma detectada: {style(os_label, '1;32')} | Carpeta base: {BASE_DIR}")
    print()

    current_category = None
    for idx, item in enumerate(utilities, start=1):
        if item.category != current_category:
            current_category = item.category
            print(style(f"[{current_category}]", "1;33"))
        desc = f" — {item.description}" if item.description else ""
        tag = style(f"({item.kind})", "90")
        print(f"  {style(f'{idx:>2})', '1;37')} {item.title} {tag} {style(item.name, '36')}{desc}")

    print()
    print("  [número o nombre] Ejecutar   [h <num>] Ver --help   [r] Recargar   [0/q] Salir")


def find_match(query: str, utilities: Iterable[UtilityScript]) -> UtilityScript | None:
    """Busca una utilidad por número, nombre exacto o coincidencia parcial."""
    items = list(utilities)
    if query.isdigit():
        index = int(query)
        if 1 <= index <= len(items):
            return items[index - 1]
        return None

    q = query.strip().lower()
    exact = [u for u in items if u.name.lower() == q or u.path.stem.lower() == q]
    if len(exact) == 1:
        return exact[0]

    partial = [u for u in items if q in u.name.lower() or q in u.title.lower()]
    if len(partial) == 1:
        return partial[0]
    if len(partial) > 1:
        print(style("Coincidencias múltiples:", "33"))
        for match in partial:
            print(f"  - {match.name}")
    return None


def interactive_loop(target_os: str) -> int:
    """Bucle principal del selector interactivo."""
    while True:
        utilities = discover_utilities(target_os)
        if not utilities:
            print(style("No se encontraron utilidades para esta plataforma.", "31"))
            return 1

        print_menu(utilities, target_os)
        try:
            choice = input(style("\nSelecciona una opción: ", "1")).strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return 0

        if not choice:
            continue
        if choice.lower() in {"0", "q", "quit", "exit", "salir"}:
            return 0
        if choice.lower() == "r":
            continue

        if choice.lower().startswith("h "):
            target = find_match(choice[2:].strip(), utilities)
            if not target:
                print(style("Opción no válida para ayuda.", "31"))
                continue
            run_utility(target, ["--help"])
            input("\nPresiona Enter para continuar...")
            continue

        selected = find_match(choice, utilities)
        if not selected:
            print(style(f"No se encontró ninguna utilidad única para '{choice}'.", "31"))
            continue

        try:
            raw_args = input("Argumentos extra (Enter para ninguno): ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            continue

        extra_args = shlex.split(raw_args, posix=(target_os != "win")) if raw_args else []
        run_utility(selected, extra_args)

        try:
            input("\nPresiona Enter para volver al menú...")
        except (EOFError, KeyboardInterrupt):
            print()
            return 0


def parse_args(argv: list[str]) -> tuple[argparse.Namespace, list[str]]:
    parser = argparse.ArgumentParser(
        description="Selector autodetectable de utilidades de monitoreo (Linux/Windows)."
    )
    parser.add_argument(
        "script",
        nargs="?",
        help="Nombre o número de la utilidad a ejecutar directamente sin menú.",
    )
    parser.add_argument(
        "--os",
        choices=("linux", "win", "all"),
        default=None,
        help="Fuerza la plataforma a escanear en lugar de autodetección.",
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="Lista las utilidades detectadas y termina.",
    )
    return parser.parse_known_args(argv)


def main(argv: list[str] | None = None) -> int:
    args, passthrough = parse_args(argv if argv is not None else sys.argv[1:])
    target_os = detect_platform(args.os)

    if args.list:
        utilities = discover_utilities(target_os)
        for idx, item in enumerate(utilities, start=1):
            desc = f" — {item.description}" if item.description else ""
            print(f"{idx:>2}. [{item.category}] {item.name}{desc}")
        return 0

    if args.script:
        utilities = discover_utilities(target_os)
        selected = find_match(args.script, utilities)
        if not selected:
            print(style(f"Utilidad no encontrada: {args.script}", "31"), file=sys.stderr)
            return 1
        clean_args = passthrough[1:] if passthrough and passthrough[0] == "--" else passthrough
        return run_utility(selected, clean_args)

    return interactive_loop(target_os)


if __name__ == "__main__":
    raise SystemExit(main())
