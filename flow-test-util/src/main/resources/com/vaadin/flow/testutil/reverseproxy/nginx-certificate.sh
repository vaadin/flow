#!/bin/sh
# Run by the entrypoint of the nginx image before nginx starts
openssl req -x509 -newkey rsa:2048 -nodes -days 2 -subj "/CN=@HOST@" \
    -keyout /etc/nginx/proxy.key -out /etc/nginx/proxy.crt
