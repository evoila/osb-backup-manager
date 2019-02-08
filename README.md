# OSB Backup Manager Framework
A typical and mandatory functionality when offering data services is the ability to do backups of your
data. As there are a lot of commercial backup tools out there, the intention of this framework is not
to replace them, but instead to provide and alternative way of enabling a customized backup experience.

## Motivation
The architecture is based on some fundamental aspects we believe are the best for a good backup strategy
* A fully usable REST API for managing the backup conditions
  * File Endpoints
  * Backup Plans
  * Restore Points
  * Backup Jobs
  * Restore Jobs
* An Agent, which needs to be included on the corresponding deployment. The agent enables the OSB Backup
Manager Framework to work with deployments on any kind of deployments
  * VMs
  * Container
  * Dedicated and Shared Cluster
* A set of Shell Scripts, which are called by the the OSB Backup Manager and enables database administrators
to provide the best suitable backup strategy/implementation for an endpoint.

### Why do we think this is a good approach?
In the last two years we have gained substantial experience in the OSBAPI enviroment, which is a framework
designed to provision Services (e.g. Database, Queueing etc.) for applications being deployed on Kubernetes
or Cloud Foundry.

What we have experienced in those environments is that customers/developers do have the need for an automated
backup mechanism, while on the other side DBAs (QA aka Queueing Administrators) have a quite good toolset to
do most suitable backup for a platform/usecase.

With the generic architecture of the OSB Backup Manager Framework we want to enable and encourage DBAs, QAs
and others to merge the best of both worlds:
* Their experience regarding the best backup technology on the platform
* The generic framework invoking the backup scheduling/backup calls and data transfer to a blockstorage

# Architecture
The following sections describe the core facts of the Framework, including implemented Datatransfer endpoints,
features of backup plans and the scripts, which need to be implemented by the DBAs.

## Supported Endpoints
* S3
* Swift

## Backup Plan
* Pausing/Unpausing plans
* Compression
* Encryption
* Frequeny (based on CRON expressions)
* Retention Period
* Retention Type (based on DAYS, HOURS, FILES)

## Scripts
The scripts being called by the OSB Backup Manger in a backup/restore run are as follows.
### Backup
The agent runs following shell scripts from top to bottom
* pre-backup-lock
* pre-backup-check
* backup
* backup-cleanup
* post-backup-unlock

In the backup stage, after the script generated the file to upload (name consists of
<host>_YYYY_MM_DD_<dbname>.tar.gz), the agent uploads the backup file from the set directory to the
cloud storage using the given information and credentials.

### Restore
The agent runs following shell scripts from top to bottom
* pre-restore-lock
* restore
* restore-cleanup
* post-restore-unlock

In the restore stage, before the dedicated script starts the actual restore, the agent downloads the
backed up restore file from the cloud storage, using the given information and credentials, and puts
it in the dedicated directory.


# References
osb-backup-agent [GitHub](https://github.com/evoila/osb-backup-agent)

# Contribution

# Authors
Yannic Remmet, evoila GmbH [GitHub Profile](https://github.com/yremmet).  
Marius Berger, evoila GmbH [GitHub Profile](https://github.com/mberger2015).  
Johannes Hiemer, evoila GmbH [GitHub Profile](https://github.com/jhiemer). 

