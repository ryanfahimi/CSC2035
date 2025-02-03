objects/test/test_jobqueue_common.o: test/test_jobqueue_common.c \
 test/test_jobqueue_common.h test/munit/munit.h test/../sim_config.h \
 test/../job.h test/../sim_config.h test/../pri_jobqueue.h test/../job.h | objects/test
	$(CC) -c $(CFLAGS) $< -o $@
