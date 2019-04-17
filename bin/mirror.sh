#!/bin/sh

git fetch --tags
mkdir -p ~/.ssh
# Ensure we can talk to GitHub
ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts
mkdir -p /tmp/.ssh
echo "${GITHUB_SSH_KEY_B64}" | base64 -d > /tmp/.ssh/github.key
chmod 600 /tmp/.ssh/github.key
# Push to GitHub mirror
git remote add github git@github.com:wisetime-io/wisetime-connector-java.git
GIT_SSH_COMMAND='ssh -i /tmp/.ssh/github.key' git push github master
GIT_SSH_COMMAND='ssh -i /tmp/.ssh/github.key' git push github --tags
rm -rf /tmp/.ssh
echo "Push to GitHub mirror complete"
