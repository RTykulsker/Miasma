#! /bin/sh


echo $(date '+%Y-%m-%d-%H-%M-%S') > pat-lockfile.txt
/usr/bin/pat  --mbox /home/bobt/Documents/miasma/pat/mailbox connect telnet