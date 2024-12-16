help:
	@echo "make help:       show this help"
	@echo "make submission: create submission zip"

submission:
	rm -rf dist
	cp -r patients dist
	
	cd report && make compile
	cp report/output/report.pdf dist
	
	cd dist && make compile
	unzip -l dist/cwk2.zip
