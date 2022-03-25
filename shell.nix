with import <nixpkgs> { };
mkShell {

  name = "env";
  buildInputs = [
    python3Packages.python
    python3Packages.venvShellHook
    figlet wasmtime wabt emscripten nodejs cmake check protobuf protobufc pkg-config jbang
  ];

  venvDir = "./.venv";
  postVenvCreation = ''
    unset SOURCE_DATE_EPOCH
    pip install wasmtime
  '';

  postShellHook = ''
    # allow pip to install wheels
    unset SOURCE_DATE_EPOCH
    figlet ":wasm:"
  '';

}