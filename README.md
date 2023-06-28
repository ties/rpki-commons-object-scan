```
# rpki-client cleans rejected files from the cache
gradle run --args "/opt/homebrew/var/cache/rpki-client"
# The routinator cache contains unvalidated files as well
gradle run --args "$HOME/.rpki-cache/repository"
```
