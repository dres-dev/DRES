#!/bin/bash

session="DRES"

tmux has-session -t $session 2>/dev/null

if [ $? != 0 ]; then
  tmux send-keys -t $session ENTER "quit" ENTER
  sleep 1
fi

tmux has-session -t $session 2>/dev/null

if [ $? != 0 ]; then
  tmux kill-session -t $session
  sleep 1
fi