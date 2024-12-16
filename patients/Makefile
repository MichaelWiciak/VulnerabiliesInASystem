help:
	@echo "make help      shows this message"
	@echo "make compile   compiles application"

compile:
	docker run \
	--rm \
	-v ./:/app \
	-w /app \
	openjdk:11 \
	/bin/sh -c "./gradlew submission"
