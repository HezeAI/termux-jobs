"""Own a single process group. Keep its leader unreaped until the last signal."""
import errno,os,select,signal,subprocess,time
class Cancelled(Exception):pass
class Refused(Exception):pass

def observe(p):
 row=os.waitid(os.P_PID,p.pid,os.WEXITED|os.WNOHANG|os.WNOWAIT)
 if row is None:return None
 return row.si_status if row.si_code==os.CLD_EXITED else 128+row.si_status

def run(args,input_data=None,cancel=lambda:False,tick=lambda:None,env=None,limit=24*1024*1024,timeout=3600):
 if not hasattr(os,'WNOWAIT'):raise Refused('OWNED_WAIT_UNAVAILABLE')
 if cancel():raise Cancelled('CANCELLED')
 # Never expose output on stdout: package scripts or source URLs may be private.
 p=subprocess.Popen(args,stdin=subprocess.PIPE if input_data is not None else subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=env,start_new_session=True)
 start=time.monotonic();data=bytearray();stopped=False;status=None;pipe=True;ended=None
 try:
  if input_data is not None:
   try:p.stdin.write(input_data);p.stdin.close()
   except BrokenPipeError:pass
  while True:
   status=observe(p)
   if cancel():stopped=True;raise Cancelled('CANCELLED')
   if time.monotonic()-start>timeout:raise Refused('PACKAGE_COMMAND_TIMEOUT')
   tick()
   if pipe and select.select([p.stdout],[],[],.15)[0]:
    block=os.read(p.stdout.fileno(),65536)
    if block:
     data.extend(block)
     if len(data)>limit:raise Refused('COMMAND_OUTPUT_TOO_LARGE')
    else:pipe=False
   elif not pipe and status is None:time.sleep(.1)
   if status is not None:
    if ended is None:ended=time.monotonic()
    if not pipe or time.monotonic()-ended>.5:break
  return status,bytes(data)
 finally:
  # No poll()/wait()/communicate() before the final killpg. An unreaped leader
  # reserves its group ID even if descendants outlive it, preventing PID reuse.
  for sig,grace in ((signal.SIGTERM,2),(signal.SIGKILL,0)):
   try:os.killpg(p.pid,sig)
   except ProcessLookupError:pass
   if grace:
    deadline=time.monotonic()+grace
    while time.monotonic()<deadline:
     try:
      if observe(p) is not None:break
     except ChildProcessError:raise Refused('OWNED_GROUP_IDENTITY_LOST') from None
     time.sleep(.05)
  p.wait()
  if p.stdout:p.stdout.close()
  if p.stdin and not p.stdin.closed:p.stdin.close()
