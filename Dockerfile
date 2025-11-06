FROM ubuntu:latest
LABEL authors="hinawatts"

ENTRYPOINT ["top", "-b"]