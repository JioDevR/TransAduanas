import argparse,ctypes,os,time
from ctypes import wintypes

p=argparse.ArgumentParser()
p.add_argument('--cpu',type=int,required=True);p.add_argument('--seconds',type=int,default=390);p.add_argument('--parent',type=int,required=True)
a=p.parse_args()
k=ctypes.WinDLL('kernel32',use_last_error=True)
k.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];k.OpenProcess.restype=wintypes.HANDLE
k.GetCurrentProcess.restype=wintypes.HANDLE
k.SetProcessAffinityMask.argtypes=[wintypes.HANDLE,ctypes.c_size_t];k.SetProcessAffinityMask.restype=wintypes.BOOL
k.WaitForSingleObject.argtypes=[wintypes.HANDLE,wintypes.DWORD];k.WaitForSingleObject.restype=wintypes.DWORD
k.CloseHandle.argtypes=[wintypes.HANDLE]
parent=k.OpenProcess(0x100000,False,a.parent)
if not parent:raise ctypes.WinError(ctypes.get_last_error())
if not k.SetProcessAffinityMask(k.GetCurrentProcess(),1<<a.cpu):raise ctypes.WinError(ctypes.get_last_error())
deadline=time.perf_counter()+min(a.seconds,400)
value=1
try:
    while time.perf_counter()<deadline and k.WaitForSingleObject(parent,0)==258:
        start=time.perf_counter()
        while time.perf_counter()-start<.095:
            for _ in range(1024):value=(value*1664525+1013904223)&0xffffffff
        time.sleep(max(0,.1-(time.perf_counter()-start)))
finally:k.CloseHandle(parent)
