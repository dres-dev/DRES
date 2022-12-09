#!/bin/bash

session="DRES"
config="config.json"

tmux has-session -t $session 2>/dev/null

if [ $? != 0 ]; then
  tmux new-session -d -s $session "./backend $config"
fi
