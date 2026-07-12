#!/usr/bin/env python3
"""Validate configreference Weight fields for plausible pounds-based values."""
from pathlib import Path
import re, sys
ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / 'configreference'
WEIGHT_RE = re.compile(r'^Weight\s*=\s*(\d+)\s*$', re.M)
DISPLAY_RE = re.compile(r'^(?:DisplayName|AddDisplayName)\s*=\s*(.+)$', re.M)

def ident(text):
    names=[m.group(1).strip() for m in DISPLAY_RE.finditer(text)]
    return ' / '.join(names).lower()

def category(path, name):
    p=str(path).replace('\\','/')
    if '/ships/' in p: return 'ship'
    if '/planes/' in p: return 'drone' if any(x in name for x in ['mavic','geran','shahed','skylark','bayraktar','mq-9','mqm','bqm','x-47b']) else 'aircraft'
    if '/helicopters/' in p: return 'drone' if any(x in name for x in ['drone','mavic','phantom','goblin','radicon','rc ']) else 'helicopter'
    if '/vehicles/' in p: return 'static'
    if any(x in name for x in ['dodge challenger','rolls-royce','nissan','toyota','mazda','porsche','bugatti','dacia','delorean','ford mondeo','subaru','mercedes-benz w123','lada','corolla','silvia','starion','phantom armored']): return 'car'
    if any(x in name for x in ['motorcycle']): return 'motorcycle'
    if any(x in name for x in ['skyline gt-r r34']): return 'car'
    if 'anti-tank gun' in name: return 'static'
    if any(x in name for x in ['tank','mbt','sherman','abrams','leopard','t-','type 10','type 74','panther','tiger','merkava','challenger 2','challenger mk','centurion','patton','pershing','kv-','is-','strv','chi-','ha-go','cromwell','hetzer','jagdtiger','jagdpanther']): return 'armor'
    if any(x in name for x in ['ifv','apc','stryker','btr','bmp','lav','m113','brdm','marder','puma','namer','rosomak']): return 'armor'
    if any(x in name for x in ['anti-tank gun','howitzer','gun','launcher','mortar','m2 browning','ags-17']): return 'static'
    if any(x in name for x in ['bicycle','jtac','spg-9']): return 'equipment'
    return 'ground'

rules={
 'car':(800,20000),'motorcycle':(100,3000),'drone':(1,50000),'helicopter':(300,80000),'aircraft':(1000,500000),'ship':(300,250000000),'armor':(10000,200000),'ground':(50,200000),'static':(10,400000),'equipment':(1,5000)
}
issues=[]; count=0
for p in sorted(CONFIG.rglob('*.txt')):
    t=p.read_text(errors='ignore')
    m=WEIGHT_RE.search(t)
    if not m: continue
    count+=1; w=int(m.group(1)); name=ident(t); cat=category(p,name); lo,hi=rules[cat]
    if w<0 or not (lo <= w <= hi):
        issues.append(f'{p.relative_to(ROOT)}: {w} lb outside {cat} plausible range {lo}-{hi} ({name[:90]})')
    if cat=='car' and w>20000: issues.append(f'{p.relative_to(ROOT)}: passenger car over 20,000 lb')
    if cat=='motorcycle' and w>3000: issues.append(f'{p.relative_to(ROOT)}: motorcycle over 3,000 lb')
    if cat=='helicopter' and w<300: issues.append(f'{p.relative_to(ROOT)}: helicopter below 300 lb')
    if cat=='armor' and w<10000: issues.append(f'{p.relative_to(ROOT)}: armored/tank vehicle below 10,000 lb')
    if cat=='ship' and w<10000 and 'zodiac' not in name: issues.append(f'{p.relative_to(ROOT)}: major ship/boat has car-scale weight')
print(f'Validated {count} Weight entries as integer pounds with category plausibility checks.')
if issues:
    print('\n'.join(issues)); sys.exit(1)
