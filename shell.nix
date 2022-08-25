with import <nixpkgs> {
  overlays = [
    (self: super: {
      # https://github.com/NixOS/nixpkgs/pull/188162
      binaryen = super.binaryen.override { nodejs = super.nodejs-14_x; };
    })
  ];
};
mkShell {

  name = "env";
  buildInputs = [
    wasmtime wabt emscripten nodejs cmake check protobuf protobufc pkg-config jbang
  ];

  shellHook = ''
    mkdir -p ~/.emscripten
    cp -rf ${emscripten}/share/emscripten/cache ~/.emscripten
    chmod +w -R ~/.emscripten
    export EM_CACHE=~/.emscripten/cache
    export TMP=/tmp
    export TMPDIR=/tmp
  '';

}