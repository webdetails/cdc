#!/bin/sh

LINT_FLAGS='--custom_jsdoc_tags=namespace --debug_indentation'
gjslint $LINT_FLAGS resources/static/client/*
