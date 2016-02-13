PATDroid also provides integrated Python tools for various analyses.
This folder contains a command line entry point (`__main__.py`) and several tools prefixed by `tool_`.
To use these tools, simply type `python tools/__main__.py` or just `python tools/`.
The command line utility has many "environment variables". Simply type "set" to see them all or set a new one.
Each tool has a single command. When invoked, the tool itself can read from these "envs" for related parameters.
After completion, the tool will update these envs for use of later commands.
Each tool should have a short description telling what it is about.

## Add a tool
New tools are welcome!

* First create your `tool_your_tool.py` and use the following snippet to integrate with the top-level CLI:

```python
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
```

* Then, add one line to the `__main__.py`, something like:
```python
cli.do_your_tool = _attach_tool(tool_your_tool.cli_entry)
```

* Finally, submit a pull request.

