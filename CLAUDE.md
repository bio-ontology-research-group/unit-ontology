# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

UO (Units of Measurement Ontology), an OBO Foundry ontology managed with the
[Ontology Development Kit](https://github.com/INCATools/ontology-development-kit) (ODK v1.6).
There is no application code — the deliverable is OWL/OBO/JSON artefacts. All tooling runs
through ROBOT inside the `obolibrary/odkfull` Docker image.

## The one rule that matters

**`src/ontology/uo-edit.owl` is the only hand-edited ontology file.** Everything else
(`uo.owl`, `uo.obo`, `uo.json`, `uo-full.*`, `uo-base.*` at the repo root, plus
`src/ontology/imports/*`) is generated. Never edit a generated artefact — the next release
overwrites it.

`uo-edit.owl` is **OWL Functional Syntax** despite the `.owl` extension (`edit_format: owl` in
the ODK config means "not obo"). Axioms are grouped per class with a `# Class: <IRI> (label)`
comment header; keep that comment in sync when a label changes.

`src/ontology/Makefile` is ODK-generated and must not be edited — put overrides in
`src/ontology/uo.Makefile`. Regenerating the Makefile means editing `src/ontology/uo-odk.yaml`
and running `sh run.sh make update_repo`.

## Commands

All commands run from `src/ontology/`. `./run.sh` wraps them in the ODK Docker container
(`export USE_SINGULARITY=1` to use Singularity instead):

```sh
cd src/ontology

# Full QC suite — same thing CI runs
./run.sh make test

# What CI actually invokes (skips import/pattern/mirror regeneration):
./run.sh make ROBOT_ENV='ROBOT_JAVA_ARGS=-Xmx6G' test IMP=false PAT=false MIR=false

# Same, shorthand
./run.sh make test_fast

# Build a release (regenerates every artefact, then copies to repo root)
./run.sh make prepare_release
./run.sh make prepare_release_fast     # skips imports/mirrors

# Refresh the PATO import module from the upstream mirror
./run.sh make refresh-imports

# Debug unsatisfiable classes
./run.sh make explain_unsat
```

`make test` is a composite of: `validate_idranges`, `reason_test` (ELK), `sparql_test`
(the `*-violation.sparql` queries in `src/sparql/`), `robot_reports`, and an OWL 2 DL profile
check. To run one piece, name it directly (e.g. `./run.sh make reason_test`).

Workflow toggles usable on any target: `IMP=false` (skip import modules), `MIR=false` (skip
remote mirror downloads), `PAT=false`, `COMP=false`. Set `ODK_DEBUG=yes` for a `debug.log`.

### Running ROBOT directly

Faster than Docker for a single check. Grab `robot.jar` from the ontodev/robot releases and
always pass the catalog so the PATO import resolves locally:

```sh
cd src/ontology
java -jar robot.jar --catalog catalog-v001.xml report --input uo-edit.owl --labels true --fail-on none
```

## Build pipeline

`uo-edit.owl` (+ `imports/pato_import.owl`)
→ merge → `reason` (ELK, `--equivalent-classes-allowed asserted-only`) → `relax` → `reduce`
→ two branches:

- **`uo-base.owl`** — additionally `remove --base-iri http://purl.obolibrary.org/obo/UO_ --axioms external`,
  i.e. UO-authored axioms only. **This is the file the OBO Dashboard grades.**
- **`uo-full.owl`** — imports merged in. `uo.owl` is `uo-full.owl` re-annotated with the
  primary release IRI; `.obo` and `.json` are converted from these.

Ontology-level annotations flow from the `Ontology(...)` header of `uo-edit.owl` into every
artefact — the release pipeline does not inject `dcterms:title`/`description`/`license`, so
they must be present in the edit file.

## Modelling conventions in this ontology

- Units are classes under `UO_0000000` (unit), each also **punned as a NamedIndividual** with
  `EquivalentClasses(X ObjectOneOf(X))`. New unit terms follow that pattern; deprecated terms
  must have it removed.
- Prefixed units are defined as
  `EquivalentClasses(X ObjectIntersectionOf(<base-unit> ObjectSomeValuesFrom(has_prefix <prefix>)))`,
  where prefixes are subclasses of `UO_0000046`.
- Definitions use `IAO_0000115` with a `oboInOwl:hasDbXref` axiom annotation for provenance.
- ID ranges are declared in `src/ontology/uo-idranges.owl`; `UO_0000000`–`UO_0999999` is the
  ontology-creator range, `UO_1000000`+ is for additional editors.

### Obsoleting a term

Deprecation must strip all logical axioms, or `robot report` raises
`deprecated_class_reference` errors. The full pattern:

- prefix the label with `obsolete ` (avoids `missing_obsolete_label` and a `duplicate_label`
  clash with the replacement term)
- `AnnotationAssertion(owl:deprecated <term> "true"^^xsd:boolean)`
- `AnnotationAssertion(<.../IAO_0100001> <term> <replacement>)` (term replaced by)
- delete the `SubClassOf`, the `EquivalentClasses(... ObjectOneOf ...)`, and the
  `Declaration(NamedIndividual(...))`

## QC expectations

`robot report` is configured (in `uo-odk.yaml`) to run on the edit file with custom SPARQL
checks from `src/sparql/`. The repo has a large standing backlog of WARN-level violations —
mostly `missing_definition` (~166), `multiple_equivalent_classes` (~80) and
`duplicate_exact_synonym` (~72) — and `fail_on` is unset, so CI does not break on them. Keep
ERROR count at zero; do not treat the WARN count as a regression signal unless you added to it.

The OBO Dashboard for this ontology is at
<https://dashboard.obofoundry.org/dashboard/uo/dashboard.html> and grades the released
`uo-base.owl`, so dashboard fixes only take effect after `prepare_release` plus a release commit.

## Release process

`prepare_release` regenerates artefacts and copies them to the repo root; the release is then
committed and merged to `master` (see the `release<DDMMYYYY>` branches in the git history).
The OBO PURL config lives in `src/metadata/uo.yml` — note it points at the **`main`** branch
while `git_main_branch` is `master`.

## Licensing note

UO is **CC BY 4.0**. This was inconsistent until 2026-07-31 (`LICENSE.md` said 4.0,
`README.md`/`uo-odk.yaml` said 2.0, the OBO Foundry registry said 3.0); all in-repo statements
plus the `dcterms:license` in the edit-file header now say 4.0. The **upstream** registry entry
at `OBOFoundry.github.io/ontology/uo.md` is a separate repo and still needs a PR to match —
until it does, the dashboard's Open check compares against the stale 3.0 value.
