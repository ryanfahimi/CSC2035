#include <stdlib.h>
#include <stdbool.h>
#include "pri_jobqueue.h"

static int find_highest_priority_job(pri_jobqueue_t* pjq) {
    if (pri_jobqueue_is_empty(pjq)) return -1;
    
    int highest_pri_idx = -1;
    unsigned int highest_pri = (unsigned int)-1;
    
    for (int i = 0; i < pjq->buf_size; i++) {
        if (pjq->jobs[i].priority == 0) continue;
        
        if (pjq->jobs[i].priority < highest_pri || highest_pri_idx == -1) {
            highest_pri = pjq->jobs[i].priority;
            highest_pri_idx = i;
        }
    }
    
    return highest_pri_idx;
}


pri_jobqueue_t* pri_jobqueue_new() {
    pri_jobqueue_t* pjq = (pri_jobqueue_t*)malloc(sizeof(pri_jobqueue_t));
    if (!pjq) return NULL;
    
    pri_jobqueue_init(pjq);
    return pjq;
}


void pri_jobqueue_init(pri_jobqueue_t* pjq) {
    if (!pjq) return;
    
    pjq->buf_size = JOB_BUFFER_SIZE;
    pjq->size = 0;
    
    for (int i = 0; i < pjq->buf_size; i++) {
        job_init(&pjq->jobs[i]);
    }
}


job_t* pri_jobqueue_dequeue(pri_jobqueue_t* pjq, job_t* dst) {
    if (pri_jobqueue_is_empty(pjq)) return NULL;
    
    int highest_pri_idx = find_highest_priority_job(pjq);
    if (highest_pri_idx == -1) return NULL;
    
    if (!dst) {
        dst = job_new(pjq->jobs[highest_pri_idx].pid,
                     pjq->jobs[highest_pri_idx].id,
                     pjq->jobs[highest_pri_idx].priority,
                     pjq->jobs[highest_pri_idx].label);
        if (!dst) return NULL;
    } else if (!job_copy(&pjq->jobs[highest_pri_idx], dst)) {
        return NULL;
    }
        
    for (int i = highest_pri_idx; i < pjq->size - 1; i++) {
        job_copy(&pjq->jobs[i + 1], &pjq->jobs[i]);
    }

    job_init(&pjq->jobs[pjq->size - 1]);
    
    pjq->size--;

    return dst;
}


void pri_jobqueue_enqueue(pri_jobqueue_t* pjq, job_t* job) {
    if (pri_jobqueue_is_full(pjq) || !job  || job->priority == 0) 
        return;
    
    if (!job_copy(job, &pjq->jobs[pjq->size])) 
        return;
    
    pjq->size++;
}
   

bool pri_jobqueue_is_empty(pri_jobqueue_t* pjq) {
    return !pjq || pjq->size == 0;
}


bool pri_jobqueue_is_full(pri_jobqueue_t* pjq) {
    return !pjq || pjq->size >= pjq->buf_size;
}


job_t* pri_jobqueue_peek(pri_jobqueue_t* pjq, job_t* dst) {
    if (pri_jobqueue_is_empty(pjq)) return NULL;
    
    int highest_pri_idx = find_highest_priority_job(pjq);
    if (highest_pri_idx == -1) return NULL;
    
    if (!dst) {
        dst = job_new(pjq->jobs[highest_pri_idx].pid,
                     pjq->jobs[highest_pri_idx].id,
                     pjq->jobs[highest_pri_idx].priority,
                     pjq->jobs[highest_pri_idx].label);
        if (!dst) return NULL;
    } else if (!job_copy(&pjq->jobs[highest_pri_idx], dst)) {
        return NULL;
    }
    
    return dst;
}


int pri_jobqueue_size(pri_jobqueue_t* pjq) {
    return pjq ? pjq->size : 0;
}


int pri_jobqueue_space(pri_jobqueue_t* pjq) {
    return pjq ? (pjq->buf_size - pjq->size) : 0;
}


void pri_jobqueue_delete(pri_jobqueue_t* pjq) {
    free(pjq);
}
