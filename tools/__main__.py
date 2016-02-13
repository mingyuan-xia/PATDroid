#!/usr/bin/env python
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
#   Mingyuan Xia
#

import cmd
import pprint
import subprocess
import tool_parse_layout


class cli(cmd.Cmd):
    prompt = "> "
    intro = "patdroid integrated tools"

    def __init__(self):
        cmd.Cmd.__init__(self)
        self.env = {}

    def do_EOF(self, line):
        """Say goodbye"""
        print("Bye")
        return True

    def do_shell(self, line):
        """ Invoke a shell command"""
        subprocess.Popen(line, shell=True).communicate()

    def do_quit(self, line):
        """Say goodbye"""
        raise SystemExit

    def do_set(self, line):
        """ set key value; if none, print all current parameters"""
        data = tuple(line.strip().split(' '))
        if len(data) == 2:
            k, v = data
            self.env[k] = v
            print('%s => %s' % (k, v))
        else:
            pprint.pprint(self.env)

    def emptyline(self):
        # do nothing
        pass

    def _update_env(self, extras):
        if extras is None:
            return
        for k in extras:
            self.env[k] = extras[k]

    def _parse_extras(self, line):
        # TODO:
        return None


def _attach_tool(tool_entry):
    def wrapper(cli, line):
        extras = cli._parse_extras(line)
        cli._update_env(extras)
        ret = tool_entry(**cli.env)
        cli._update_env(ret)
    wrapper.__doc__ = tool_entry.__doc__
    return wrapper

# all integrated tools
cli.do_parse_layout = _attach_tool(tool_parse_layout.cli_entry)

if __name__ == "__main__":
    cli().cmdloop()
