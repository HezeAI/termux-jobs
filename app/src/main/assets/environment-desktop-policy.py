"""Choose desktop package source from an inode-bound private origin receipt."""
import json,os,re,stat,sys
from pathlib import Path
TARGETS={'ubuntu','debian','kali','alpine','fedora','opensuse','void','archlinux','manjaro'}
def official(files,prefix,target):
 if target not in TARGETS:return False
 p=Path(prefix);files=Path(files)
 if p.name!='usr' or not p.resolve(strict=True).parent.samefile(files):raise ValueError('PRIVATE_STORAGE_ALIAS')
 flags=os.O_RDONLY|os.O_DIRECTORY|os.O_NOFOLLOW;opened=[]
 def descend(root,parts):
  fd=os.open(root,flags);opened.append(fd)
  for part in parts:fd=os.open(part,flags,dir_fd=fd);opened.append(fd)
  return fd
 try:
  container=descend(p.resolve(strict=True),['var','lib','proot-distro','containers',target]);st=os.fstat(container)
  directory=descend(files.resolve(strict=True),['workspace-environment-updates','origins'])
  fd=os.open(target+'.json',os.O_RDONLY|os.O_NOFOLLOW|os.O_NONBLOCK,dir_fd=directory)
  with os.fdopen(fd,'rb') as f:
   info=os.fstat(f.fileno())
   if not stat.S_ISREG(info.st_mode) or info.st_nlink!=1 or info.st_size>128*1024:raise ValueError('CONTROL_FILE')
   row=json.load(f)
  return row.get('containerId')==[st.st_dev,st.st_ino] and isinstance(row.get('image'),dict) and row['image'].get('channel')=='official'
 except FileNotFoundError:return False
 finally:
  for fd in reversed(opened):os.close(fd)
if __name__=='__main__':
 try:
  if len(sys.argv)!=4:raise ValueError('ARGUMENTS')
  print('official' if official(sys.argv[1],sys.argv[2],sys.argv[3]) else 'pinned')
 except (OSError,ValueError,TypeError):sys.exit(2)
