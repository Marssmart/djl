
![DeepJavaLibrary](website/img/deepjavalibrary.png?raw=true "Deep Java Library")

![](https://github.com/awslabs/djl/workflows/nightly%20build/badge.svg)

# Deep Java Library (DJL)

## Overview

Deep Java Library (DJL) is an open-source, high-level, engine-agnostic Java framework for deep learning. DJL is designed to be easy to get started with and simple to
use for Java developers. DJL provides a native Java development experience and functions like any other regular Java library.

You don't have to be machine learning/deep learning expert to get started. You can use your existing Java expertise as an on-ramp to learn and use machine learning and deep learning. You can
use your favorite IDE to build, train, and deploy your models. DJL makes it easy to integrate these models with your
Java applications.

Because DJL is deep learning engine agnostic, you don't have to make a choice
between engines when creating your projects. You can switch engines at any
point. To ensure the best performance, DJL also provides automatic CPU/GPU choice based on hardware configuration.

DJL's ergonomic API interface is designed to guide you with best practices to accomplish
deep learning tasks.
The following pseudocode demonstrates running inference:

```java
    // Assume user uses a pre-trained model from model zoo, they just need to load it
    Criteria<Image, Classifications> criteria =
            Criteria.builder()
                    .optApplication(Application.CV.OBJECT_DETECTION) // find object dection model
                    .setTypes(Image.class, Classifications.class) // define input and output
                    .optFilter("backbone", "resnet50") // choose network architecture
                    .build();

    try (ZooModel<Image, Classifications> model = ModelZoo.loadModel(criteria)) {
        try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
            Image img = ImageFactory.getInstance().fromUrl("http://..."); // read image
            Classifications result = predictor.predict(img);

            // get the classification and probability
            ...
        }
    }
```

The following pseudocode demonstrates running training:

```java
    // Construct your neural network with built-in blocks
    Block block = new Mlp(28, 28);

    try (Model model = Model.newInstance()) { // Create an empty model
        model.setBlock(block); // set neural network to model

        // Get training and validation dataset (MNIST dataset)
        Dataset trainingSet = new Mnist.Builder().setUsage(Usage.TRAIN) ... .build();
        Dataset validateSet = new Mnist.Builder().setUsage(Usage.TEST) ... .build();

        // Setup training configurations, such as Initializer, Optimizer, Loss ...
        TrainingConfig config = setupTrainingConfig();
        try (Trainer trainer = model.newTrainer(config)) {
            /*
             * Configure input shape based on dataset to initialize the trainer.
             * 1st axis is batch axis, we can use 1 for initialization.
             * MNIST is 28x28 grayscale image and pre processed into 28 * 28 NDArray.
             */
            Shape inputShape = new Shape(1, 28 * 28);
            trainer.initialize(new Shape[] {inputShape});

            TrainingUtils.fit(trainer, epoch, trainingSet, validateSet);
        }

        // Save the model
        model.save(modelDir, "mlp");
    }
```

## [Getting Started](docs/quick_start.md)

## Resources
- [Documentation](docs/README.md#documentation)
- [JavaDoc API Reference](https://javadoc.djl.ai/)

## Release Notes
* [0.6.0](https://github.com/awslabs/djl/releases/tag/v0.6.0) ([Code](https://github.com/awslabs/djl/tree/v0.6.0))
* [0.5.0](https://github.com/awslabs/djl/releases/tag/v0.5.0) ([Code](https://github.com/awslabs/djl/tree/v0.5.0))
* [0.4.0](https://github.com/awslabs/djl/releases/tag/v0.4.0) ([Code](https://github.com/awslabs/djl/tree/v0.4.0))
* [0.3.0](https://github.com/awslabs/djl/releases/tag/v0.3.0) ([Code](https://github.com/awslabs/djl/tree/v0.3.0))
* [0.2.1](https://github.com/awslabs/djl/releases/tag/v0.2.1) ([Code](https://github.com/awslabs/djl/tree/v0.2.1))
* [0.2.0 Initial release](https://github.com/awslabs/djl/releases/tag/v0.2.0) ([Code](https://github.com/awslabs/djl/tree/v0.2.0))

## Building From Source

To build from source, begin by checking out the code.
Once you have checked out the code locally, you can build it as follows using Gradle:

```sh
./gradlew build
```

To increase build speed, you can use the following command to skip unit tests:
```sh
./gradlew build -x test
```

**Note:** SpotBugs is not compatible with JDK 11+. SpotBugs will not be executed if you are using JDK 11+.

## Slack channel

Join our [<img src='https://cdn3.iconfinder.com/data/icons/social-media-2169/24/social_media_social_media_logo_slack-512.png' width='20px' /> slack channel](https://join.slack.com/t/deepjavalibrary/shared_invite/zt-ar91gjkz-qbXhr1l~LFGEIEeGBibT7w) to get in touch with the development team, for questions and discussions.

## License

This project is licensed under the Apache-2.0 License.
