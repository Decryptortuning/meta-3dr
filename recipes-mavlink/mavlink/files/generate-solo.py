#!/usr/bin/env python3
"""Generate Python and C from one explicitly owned Solo XML closure."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import sys

source, xml_root, headers = (Path(x).resolve() for x in sys.argv[1:])
if not os.environ.get('SOURCE_DATE_EPOCH', '').isdigit():
    raise SystemExit('A reproducible source epoch is required')
spec = importlib.util.spec_from_file_location('pymavlink', source / '__init__.py',
                                            submodule_search_locations=[str(source)])
package = importlib.util.module_from_spec(spec)
sys.modules['pymavlink'] = package
spec.loader.exec_module(package)
from pymavlink.generator import mavgen, mavparse

for version, folder in ((mavparse.PROTOCOL_1_0, 'v10'), (mavparse.PROTOCOL_2_0, 'v20')):
    output = source / 'dialects' / folder
    for path in xml_root.glob('*.xml'):
        shutil.copyfile(path, output / path.name)
    options = mavgen.Opts(str(output / 'solo.py'), wire_protocol=version,
                         language='Python3', validate=True)
    if not mavgen.mavgen(options, [str(xml_root / 'solo.xml')]):
        raise SystemExit('Solo Python generation failed')

options = mavgen.Opts(str(headers), wire_protocol=mavparse.PROTOCOL_1_0,
                     language='C', validate=True)
if not mavgen.mavgen(options, [str(xml_root / 'solo.xml')]):
    raise SystemExit('Solo C generation failed')
manifest = {'generator_version': package.__version__, 'dialect': 'solo',
            'c_wire_protocol': '1.0', 'python_wire_protocols': ['1.0', '2.0'],
            'legacy_source_revision': '3887c3f7b491b41b02a6061dfd4a1f8f54162da7',
            'xml_sha256': {p.name: hashlib.sha256(p.read_bytes()).hexdigest()
                           for p in sorted(xml_root.glob('*.xml'))}}
(headers.parent / 'solo-contract.json').write_text(json.dumps(manifest, indent=2, sort_keys=True) + '\n')
