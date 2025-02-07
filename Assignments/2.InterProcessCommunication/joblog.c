
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <errno.h>
#include "joblog.h"


static char* new_log_name(proc_t* proc) {
    static char* joblog_name_fmt = "%s/%.31s%07d.txt";
                                // string format for the name of a log file
                                // declared static to have only one instance

    if (!proc)
        return NULL;

    char* log_name;
            
    asprintf(&log_name, joblog_name_fmt, JOBLOG_PATH, proc->type_label,
        proc->id);

    return log_name;
}


int joblog_init(proc_t* proc) {
    if (!proc) {
        errno = EINVAL;
        return -1;
    }
        
    int r = 0;
    if (proc->is_init) {
        struct stat sb;
    
        if (stat(JOBLOG_PATH, &sb) != 0) {
            if (!S_ISDIR(sb.st_mode)) {
                unlink(JOBLOG_PATH);
            }
            r = mkdir(JOBLOG_PATH, 0777);
        }
    }

    joblog_delete(proc);
    
    return r;
}


job_t* joblog_read(proc_t* proc, int entry_num, job_t* job) {
    int original_errno = errno;
    
    if (!proc || entry_num < 0) {
        errno = original_errno;
        return NULL;
    }
    
    char* log_name = new_log_name(proc);
    if (!log_name) {
        errno = original_errno;
        return NULL;
    }
    
    FILE* file = fopen(log_name, "r");
    free(log_name);
    
    if (!file) {
        errno = original_errno;
        return NULL;
    }
    
    if (fseek(file, entry_num * JOB_STR_SIZE, SEEK_SET) != 0) {
        fclose(file);
        errno = original_errno;
        return NULL;
    }
    
    char line[JOB_STR_SIZE];
    if (!fgets(line, JOB_STR_SIZE, file)) {
        fclose(file);
        errno = original_errno;
        return NULL;
    }
    
    fclose(file);
    job_t* result = str_to_job(line, job);
    errno = original_errno;
    return result;
}


void joblog_write(proc_t* proc, job_t* job) {
    int original_errno = errno;
    
    if (!proc || !job) {
        errno = original_errno;
        return;
    }
    
    char* log_name = new_log_name(proc);
    if (!log_name) {
        errno = original_errno;
        return;
    }
    
    FILE* file = fopen(log_name, "a");
    free(log_name);
    
    if (!file) {
        errno = original_errno;
        return;
    }
    
    char job_str[JOB_STR_SIZE + 1];
    if (!job_to_str(job, job_str)) {
        fclose(file);
        errno = original_errno;
        return;
    }
    
    if (fprintf(file, "%s\n", job_str) < 0) {
        fclose(file);
        errno = original_errno;
        return;
    }
    
    fclose(file);
    errno = original_errno;
}


void joblog_delete(proc_t* proc) {
    if (!proc) return;
    
    int original_errno = errno;
    
    char* log_name = new_log_name(proc);
    if (log_name) {
        remove(log_name);
        free(log_name);
    }
    
    errno = original_errno;
}