#!/bin/bash

for file in *.shader; do
    xxd -i "$file" "$file.h";
done;
