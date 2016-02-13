# -*- coding: utf-8 -*-

#
# Copyright 2016 PATDroid project (https://github.com/mingyuan-xia/PATDroid)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Contributors:
#   Zhuocheng Ding
#   Mingyuan Xia
#

import xml.etree.ElementTree as Et
import json
import os


class LayoutAnalyzer:
    def __init__(self, res_dir):
        self._res_dir = res_dir
        self.on_clicks = set()
        self.classes = set()

    def parse(self, layout):
        tree = Et.parse(layout)
        for e in tree.iter():
            if e.tag not in ["include", "merge", "requestFocus", "view", "fragment"]:
                self.classes.add(e.tag)
            if e.tag == "view":
                clazz = e.get("{http://schemas.android.com/apk/res/android}class", None)
                if clazz:
                    self.classes.add(clazz)
            if e.tag == "fragment":
                clazz = e.get("{http://schemas.android.com/apk/res/android}name", None)
                if clazz:
                    self.classes.add(clazz)
            on_click = e.get("{http://schemas.android.com/apk/res/android}onClick", None)
            if on_click:
                self.on_clicks.add(on_click)

    def analyze(self):
        """
        analyze all xml files under 'layout' or 'layout-xxx' directory to collect useful information
        """
        layout_dirs = [d for d in os.listdir(self._res_dir)
                       if d.startswith("layout") and os.path.isdir(os.path.join(self._res_dir, d))]
        for layout_dir in layout_dirs:
            for root, _, files in os.walk(os.path.join(self._res_dir, layout_dir)):
                for f in files:
                    _, ext = os.path.splitext(f)
                    if ext != ".xml":
                        continue
                    layout = os.path.join(root, f)
                    self.parse(layout)

    def output(self, layout_json):
        with open(layout_json, "w") as f:
            json.dump({"external_methods": {"onClick": list(self.on_clicks)},
                       "classes": list(self.classes)}, f, indent=4)


def cli_entry(disassembled=None, layout_database='layout_database.json', **kwargs):
    """ Generate the layout database from a disassembled apk"""
    if disassembled is None:
        print('Must provide "disassembled": the path to the disassembled apk folder')
        return
    if layout_database is None:
        print('Must provide "layout_database": the file path to store the parsed layout database')
        return
    res_dir = os.path.join(disassembled, 'res')
    layout_json = layout_database
    analyzer = LayoutAnalyzer(res_dir)
    analyzer.analyze()
    analyzer.output(layout_json)
    print('Done, saved to %s' % (layout_database, ))
    ret = {
        'layout_database': layout_database
    }
    return ret
