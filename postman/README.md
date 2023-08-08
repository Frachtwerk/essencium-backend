# Essencium Postman Collection

Essencium comes with a Postman collection that provides pre-built request examples for every endpoint offered by the essencium's backend. In addition, a Postman [Pre-Request Script](https://learning.postman.com/docs/writing-scripts/pre-request-scripts/) runs before every request to automatically fetch a new token, that is referenced in authenticated request with `{{TOKEN}}`. This way, you do not have to manually fetch, copy and paste a token for every request you make it Postman.

## How to use
### Step 1: Import collection
![](../doc/assets/postman1.png)

1. Go to `Import` (top left, next to `New`)
1. Select the file `postman/Essencium Backend.postman_collection.json`
1. Import it

### Step 2: Import environment
![](../doc/assets/postman2.png)

1. Click the settings icon (top right, next to the 👁 icon)
1. Click `Import`
1. Select the file `Essencium Environment.postman_environment.json`
1. Import it
1. Choose it from the environment selection dropdown menu in the top right 

## Automated API-Testing with `newman`

see [/docker/newman/README.md](../docker/newman/README.md)
Simply run `postman/build_docker_image.sh`.
