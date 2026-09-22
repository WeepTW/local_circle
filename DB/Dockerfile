FROM golang:1.26.8-alpine3.24 AS gosu
WORKDIR /src
ADD https://github.com/tianon/gosu/archive/refs/tags/1.19.tar.gz /tmp/gosu.tar.gz
RUN echo 'cd9719b775dbfedae53923c9b0dc792b66d42c51e0b36652ed6f747fbadc0164  /tmp/gosu.tar.gz' | sha256sum -c - \
    && tar -xzf /tmp/gosu.tar.gz --strip-components=1 \
    && CGO_ENABLED=0 go build -trimpath -ldflags='-s -w' -o /out/gosu .

FROM mysql:8.4.11
RUN microdnf update -y && microdnf clean all
COPY --from=gosu /out/gosu /usr/local/bin/gosu
COPY --from=gosu /src/LICENSE /usr/share/licenses/gosu/LICENSE
RUN chmod 0755 /usr/local/bin/gosu && gosu nobody true
