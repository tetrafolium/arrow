package arrow.dagger.effects.instances.rx2

import arrow.effects.*
import arrow.typeclasses.*
import dagger.Module
import dagger.Provides

@Module
class FlowableKInstances {

    @Provides
    fun flowableKFunctor(): Functor<ForFlowableK> = FlowableK.functor()

    @Provides
    fun flowableKApplicative(): Applicative<ForFlowableK> = FlowableK.applicative()

    @Provides
    fun flowableKApplicativeError(): ApplicativeError<ForFlowableK, Throwable> = FlowableK.applicativeError()

    @Provides
    fun flowableKMonad(): Monad<ForFlowableK> = FlowableK.monad()

    @Provides
    fun flowableKMonadError(): MonadError<ForFlowableK, Throwable> = FlowableK.monadError()

    @Provides
    fun flowableKMonadSuspend(): MonadSuspend<ForFlowableK> = FlowableK.monadSuspend()

    @Provides
    fun flowableKAsync(): Async<ForFlowableK> = FlowableK.async()

    @Provides
    fun flowableKEffect(): Effect<ForFlowableK> = FlowableK.effect()
}
