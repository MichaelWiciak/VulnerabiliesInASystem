help:
	@echo "make help      shows this message"
	@echo "make compile   compiles the report"

compile:
	docker run \
	--rm \
	-v ./:/report \
	-w /report \
	ghcr.io/typst/typst:latest \
	/bin/sh -c "mkdir -p output && typst compile 'main.typ' 'output/report.pdf'"
