#!/usr/bin/env python3
import argparse,glob,subprocess,os
P=argparse.ArgumentParser();P.add_argument('--lang',choices=['py','c','rs','sh'],required=True);P.add_argument('--method',required=True);P.add_argument('--seed',default='rmr/rrr/Rafael_Rafael_semente.txt');P.add_argument('--value',default='1');a=P.parse_args();
path=f"tools/state_geometry_lab/modules/{a.lang}/method_{int(a.method):03d}.{a.lang if a.lang!='sh' else 'sh'}"
if a.lang=='py': subprocess.check_call(['python3',path,'--seed',a.seed,'--value',a.value])
elif a.lang=='sh': subprocess.check_call(['bash',path,a.seed,a.value])
else: print(path)
