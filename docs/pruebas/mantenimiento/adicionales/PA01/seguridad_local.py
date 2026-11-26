from pathlib import Path
import ctypes,hashlib
from ctypes import wintypes
def sha(p):
    h=hashlib.sha256()
    with Path(p).open('rb') as f:
        for b in iter(lambda:f.read(1048576),b''):h.update(b)
    return h.hexdigest()
class Blob(ctypes.Structure):
    _fields_=[('cbData',wintypes.DWORD),('pbData',ctypes.POINTER(ctypes.c_ubyte))]
def dpapi(data,protect=True):
    buf=(ctypes.c_ubyte*len(data)).from_buffer_copy(data)
    incoming=Blob(len(data),buf);outgoing=Blob()
    crypt=ctypes.WinDLL('crypt32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    kernel.LocalFree.argtypes=[ctypes.c_void_p];kernel.LocalFree.restype=ctypes.c_void_p
    if protect:
        fn=crypt.CryptProtectData;fn.argtypes=[ctypes.POINTER(Blob),wintypes.LPCWSTR,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,wintypes.DWORD,ctypes.POINTER(Blob)]
        ok=fn(ctypes.byref(incoming),'TransAduanas PM11 AES key',None,None,None,1,ctypes.byref(outgoing))
    else:
        fn=crypt.CryptUnprotectData;fn.argtypes=[ctypes.POINTER(Blob),ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,wintypes.DWORD,ctypes.POINTER(Blob)]
        ok=fn(ctypes.byref(incoming),None,None,None,None,1,ctypes.byref(outgoing))
    if not ok:raise ctypes.WinError(ctypes.get_last_error())
    try:return ctypes.string_at(outgoing.pbData,outgoing.cbData)
    finally:kernel.LocalFree(ctypes.cast(outgoing.pbData,ctypes.c_void_p))
