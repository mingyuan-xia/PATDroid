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


# a module-level cli_entry function, to be integrated by the top-level patdroid cli
# it must use all keyword parameters
def cli_entry(arg1=None, arg2='some string', **kwargs):
    """ the pydoc will be displayed as the command description"""
    # check arguments
    if arg1 is None:
        print('Must provide "arg1": explain arg1')
        return
    if arg2 is None:
        print('Must provide "arg2": explain arg2, default value: "some string"')
        return
    # do the work here
    # better print something indicating the progress and the final result
    # but dont print too much
    ret = {
        'param_to_update': 'value'
    }
    # instruct the cli to update some parameters (e.g., the output file path)
    # these updates will be available to later commands
    # if return value is None, patdroid cli will do nothing
    return ret
