#!/usr/bin/env python3
import json,argparse

if __name__=="__main__":
 p=argparse.ArgumentParser();p.add_argument("--seed",default="rmr/rrr/Rafael_Rafael_semente.txt");p.add_argument("--value",type=int,default=18);a=p.parse_args();
 print(json.dumps({"method":"py_018","seed":a.seed,"value":a.value}))
