objects/test/test_joblog.o: test/test_joblog.c test/test_joblog.h test/munit/munit.h \
 test/procs4tests.h test/../proc.h test/../sim_config.h test/../joblog.h \
 test/../job.h test/../proc.h | objects/test
	$(CC) -c $(CFLAGS) $< -o $@
