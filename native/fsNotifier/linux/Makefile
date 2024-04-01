fsnotifier: main.c inotify.c util.c fsnotifier.h
	clang -O2 -Wall -Wextra -Wpedantic -D "VERSION=\"$VER\"" -std=c11 main.c inotify.c util.c -o fsnotifier

clean:
	rm fsnotifier

all: fsnotifier
