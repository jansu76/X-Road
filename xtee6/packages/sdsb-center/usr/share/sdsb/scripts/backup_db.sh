#!/bin/sh

PGPASSWORD=centerui pg_dump -F t -h 127.0.0.1 -U centerui -f /var/lib/sdsb/dbdump.dat centerui_production
