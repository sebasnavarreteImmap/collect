# Contributing code to ODK Collect

This is a living document. If you see something that could be improved, edit this document and submit a pull request following the instructions below!

## Submitting a pull request
To contribute code to ODK Collect, you will need to open a [pull request](https://help.github.com/articles/about-pull-requests/) which will be reviewed by the community and then merged into the core project.

1. [Set up your development environment](https://github.com/opendatakit/collect#setting-up-your-development-environment). 

1. To make sure you have the latest version of the code, set up this repository as [a remote for your fork](https://help.github.com/articles/configuring-a-remote-for-a-fork/) and then [sync your fork](https://help.github.com/articles/syncing-a-fork/).

1. Create a branch for the code you will be writing:

        git checkout -b NAME_OF_YOUR_BRANCH

1. If there is an [issue](https://github.com/opendatakit/collect/issues) corresponding to what you will work on, **comment on it** to say you are addressing it. 

1. Each time you make incremental progress towards your goal, commit your changes with a meaningful commit message:

        git commit -m "add a description of the changes here"

1. Push changes to your fork to make them publicly available:

        git push

1. When your changes are ready to be added to the core ODK Collect project, [open a pull request](https://help.github.com/articles/creating-a-pull-request/). Make sure to set the base fork to `opendatakit/collect`. Describe your changes in the comment, refer to any relevant issues and tag any person you think might need to know about the changes.

## Style guidelines
For now, match the style of the code in the file you are editing. When creating new files, follow the [Android style rules](http://source.android.com/source/code-style.html).