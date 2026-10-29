"""Validated remote baseline fetch, supervised by the update process-group owner."""
import contextlib,fcntl,importlib.util,json,os,stat,sys
from pathlib import Path
def load(name):
 spec=importlib.util.spec_from_file_location(name.replace('-','_'),Path(__file__).with_name(name+'.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
T=load('environment-update-tree');I=load('environment-update-images');F=load('environment-image')
def main():
 if len(sys.argv)!=4:raise ValueError('ARGUMENTS')
 target,sha,prefix=sys.argv[1:];item=I.latest()[target]
 if item['sha256']!=sha:raise ValueError('IMAGE_PLAN_CHANGED')
 root=T.m.open_dir(Path(prefix).resolve(strict=True).parent)
 try:
  control=T.mkdir(root,'.environment-control',private=False);images=T.mkdir(control,'images',private=False);release=T.mkdir(images,item['release'],private=False);directory=T.mkdir(release,target,private=False)
  try:
   fd=os.open('download.lock',os.O_RDWR|os.O_CREAT|os.O_NOFOLLOW,0o600,dir_fd=directory)
   try:
    st=os.fstat(fd)
    if not stat.S_ISREG(st.st_mode) or st.st_nlink!=1:raise ValueError('CONTROL_FILE')
    fcntl.flock(fd,fcntl.LOCK_EX|fcntl.LOCK_NB)
    path=Path(prefix).resolve(strict=True).parent/'.environment-control/images'/item['release']/target/item['filename']
    with contextlib.redirect_stdout(sys.stderr):F.download(path,item['url'],sha,item['size'])
   finally:os.close(fd)
  finally:
   for fd in (control,images,release,directory):os.close(fd)
 finally:os.close(root)
if __name__=='__main__':
 try:main()
 except Exception:sys.exit(1)
