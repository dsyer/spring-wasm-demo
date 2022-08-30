ARG VARIANT=17-bullseye
FROM mcr.microsoft.com/vscode/devcontainers/java:0-${VARIANT}

RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash -
# [Optional] Uncomment this section to install additional OS packages.
RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get -y install --no-install-recommends curl make unzip xz-utils acl

ARG USER=vscode
ARG USER_ID=1000
ARG USER_GID=1000

RUN mkdir -m 0755 /nix && chown ${USER_ID}:${USER_GID} /nix

USER ${USER_ID}:${USER_GID}
RUN curl -L https://nixos.org/nix/install | sh
