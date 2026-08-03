FROM postgres:15

COPY backup.sh /usr/local/bin/backup.sh
COPY scheduler.sh /usr/local/bin/scheduler.sh
RUN chmod 0755 /usr/local/bin/backup.sh /usr/local/bin/scheduler.sh

# Runs once per day at BACKUP_TIME (02:30 by default).
CMD ["/usr/local/bin/scheduler.sh"]
