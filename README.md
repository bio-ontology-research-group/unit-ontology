# Unit Ontology: An ontology of units of measurements

The ontology is provided under a [CC-BY](https://creativecommons.org/licenses/by/2.0/) license.

![Build Status](https://github.com/bio-ontology-research-group/unit-ontology/workflows/CI/badge.svg)

Description: An ontology of units of measurements

⚠️ UO is a community project that is being maintained by its users. If you use UO and need some changes in its contents (for example you need some new terms), the surest way of making the changes happen is to implement them yourself and to submit a pull request. There is no “UO core team” specifically in charge of meeting users’ requests.

More information can be found at http://obofoundry.org/ontology/uo

## Versions

### Stable release versions

The latest version of the ontology can always be found at:

http://purl.obolibrary.org/obo/uo.owl

(note this will not show up until the request has been approved by obofoundry.org)

### Making releases
There is no schedule for releases, but if 3-4 changes have been merged a release will probably soon come after. Releases may also be made on request.

The process for making releases:

- Ensure you have latest ODK installed (docker pull obolibrary/odkfull)
- Create new branch, e.g. release20260101
- `cd src/ontology`
- `sh run.sh make prepare_release`
- Commit changes to branch with a meaningful message
- Push and open PR

### Editors' version

Editors of this ontology should use the edit version, [src/ontology/uo-edit.owl](src/ontology/uo-edit.owl)

## Contact

Please use this GitHub repository's [Issue tracker](https://github.com/bio-ontology-research-group/unit-ontology/issues) to request new terms/classes or report errors or specific concerns related to the ontology.

## Acknowledgements

This ontology repository was created using the [Ontology Development Kit (ODK)](https://github.com/INCATools/ontology-development-kit).
