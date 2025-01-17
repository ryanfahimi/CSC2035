/*
 * Replace the following string of 0s with your student number
 * 240653709
 */
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "job.h"

static void process_label(const char* src_label, char* dst_label) {
    if (!src_label || !src_label[0]) {
        snprintf(dst_label, MAX_NAME_SIZE, "%s", PAD_STRING);
        return;
    }
    
    strncpy(dst_label, src_label, MAX_NAME_SIZE-1);
    
    size_t len = strlen(dst_label);
    while (len < MAX_NAME_SIZE-1) {
        dst_label[len++] = '*';
    }
    dst_label[MAX_NAME_SIZE-1] = '\0';
}

/* 
 * DO NOT EDIT the job_new function.
 */
job_t* job_new(pid_t pid, unsigned int id, unsigned int priority, 
    const char* label) {
    return job_set((job_t*) malloc(sizeof(job_t)), pid, id, priority, label);
}

/* 
 * TODO: you must implement this function
 */
job_t* job_copy(job_t* src, job_t* dst) {    
    if (!src) return NULL;

    if (strlen(src->label) != MAX_NAME_SIZE - 1) return NULL;

    if (src == dst) return src;

    if (!dst) {
        dst = (job_t*)malloc(sizeof(job_t));
        if (!dst) return NULL;
    }

    dst->pid = src->pid;
    dst->id = src->id;
    dst->priority = src->priority;
    strncpy(dst->label, src->label, MAX_NAME_SIZE);

    return dst;
}

/* 
 * TODO: you must implement this function
 */
void job_init(job_t* job) {
    if (!job) return;
    
    job->pid = 0;
    job->id = 0;
    job->priority = 0;
    strncpy(job->label, PAD_STRING, MAX_NAME_SIZE);
}

/* 
 * TODO: you must implement this function
 */
bool job_is_equal(job_t* j1, job_t* j2) {
    if (j1 == j2) return true;
    if (!j1 || !j2) return false;
    
    return j1->pid == j2->pid &&
           j1->id == j2->id &&
           j1->priority == j2->priority &&
           strncmp(j1->label, j2->label, MAX_NAME_SIZE) == 0;
}


/*
 * TODO: you must implement this function.
 * Hint:
 * - read the information in job.h about padding and truncation of job
 *      labels
 */
job_t* job_set(job_t* job, pid_t pid, unsigned int id, unsigned int priority,
    const char* label) {
    if (!job) return NULL;
    
    job->pid = pid;
    job->id = id;
    job->priority = priority;
    process_label(label, job->label);
    
    return job;
}


/*
 * TODO: you must implement this function.
 * Hint:
 * - see malloc and calloc system library functions for dynamic allocation, 
 *      and the documentation in job.h for when to do dynamic allocation
 */
char* job_to_str(job_t* job, char* str) {
    if (!job || strlen(job->label) != MAX_NAME_SIZE - 1) return NULL;
    
    char* buffer = str ? str : (char*)malloc(JOB_STR_SIZE);
    if (!buffer) return NULL;
    
    snprintf(buffer, JOB_STR_SIZE, JOB_STR_FMT,
             job->pid, job->id, job->priority, job->label);
    
    return buffer;
}

/*
 * TODO: you must implement this function.
 * Hint:
 * - see the hint for job_to_str
 */
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

/* 
 * TODO: you must implement this function
 * Hint:
 * - look at the allocation of a job in job_new
 */
void job_delete(job_t* job) {
    free(job);
}