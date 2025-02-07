#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "job.h"


job_t* job_new(pid_t pid, unsigned int id, unsigned int priority, 
    const char* label) {
    return job_set((job_t*) malloc(sizeof(job_t)), pid, id, priority, label);
}


job_t* job_copy(job_t* src, job_t* dst) {    
    if (!src) return NULL;

    if (strnlen(src->label, MAX_NAME_SIZE) != MAX_NAME_SIZE - 1) return NULL;

    if (src == dst) return src;

    if (!dst) {
        return job_new(src->pid, src->id, src->priority, src->label);
    }

    dst->pid = src->pid;
    dst->id = src->id;
    dst->priority = src->priority;
    strncpy(dst->label, src->label, MAX_NAME_SIZE);

    return dst;
}


void job_init(job_t* job) {
    if (!job) return;
    
    job->pid = 0;
    job->id = 0;
    job->priority = 0;
    strncpy(job->label, PAD_STRING, MAX_NAME_SIZE);
}


bool job_is_equal(job_t* j1, job_t* j2) {
    if (j1 == j2) return true;
    if (!j1 || !j2) return false;
    
    return j1->pid == j2->pid &&
           j1->id == j2->id &&
           j1->priority == j2->priority &&
           strncmp(j1->label, j2->label, MAX_NAME_SIZE) == 0;
}


job_t* job_set(job_t* job, pid_t pid, unsigned int id, unsigned int priority,
    const char* label) {
    if (!job) return NULL;
    
    job->pid = pid;
    job->id = id;
    job->priority = priority;
    if (!label || !label[0]) {
        snprintf(job->label, MAX_NAME_SIZE, "%s", PAD_STRING);
    } else {
        char temp[MAX_NAME_SIZE];
        snprintf(temp, MAX_NAME_SIZE, "%s", label);
        size_t len = strnlen(temp, MAX_NAME_SIZE);
        while (len < MAX_NAME_SIZE - 1) {
            temp[len++] = '*';
        }
        temp[MAX_NAME_SIZE - 1] = '\0';
        strncpy(job->label, temp, MAX_NAME_SIZE);
    }

    return job;
}


char* job_to_str(job_t* job, char* str) {
    if (!job || strlen(job->label) != MAX_NAME_SIZE - 1) return NULL;
    
    char* buffer = str ? str : (char*)calloc(JOB_STR_SIZE, sizeof(char));
    if (!buffer) return NULL;
    
    if (snprintf(buffer, JOB_STR_SIZE, JOB_STR_FMT,
        job->pid, job->id, job->priority, job->label) < 0) {
            if (!str) free(buffer);
            return NULL;
        }
    
    return buffer;
}


job_t* str_to_job(char* str, job_t* job) {
    if (!str || strlen(str) != JOB_STR_SIZE - 1) return NULL;
    
    job_t* result = job ? job : (job_t*)malloc(sizeof(job_t));
    if (!result) return NULL;
    
    pid_t pid;
    unsigned int id, priority;
    char label[MAX_NAME_SIZE];
    
    if (sscanf(str, JOB_STR_FMT, &pid, &id, &priority, label) != 4) {
        if (!job) free(result);
        return NULL;
    }
    
    result->pid = pid;
    result->id = id;
    result->priority = priority;
    strncpy(result->label, label, MAX_NAME_SIZE);
    
    return result;
}

void job_delete(job_t* job) {
    free(job);
}