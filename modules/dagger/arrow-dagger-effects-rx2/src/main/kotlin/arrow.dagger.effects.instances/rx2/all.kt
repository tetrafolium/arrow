package arrow.dagger.effects.instances.rx2

import dagger.Module

@Module(
    includes = [
        ObservableKInstances::class,
        FlowableKInstances::class
    ]
)
abstract class ArrowEffectsRx2Instances
