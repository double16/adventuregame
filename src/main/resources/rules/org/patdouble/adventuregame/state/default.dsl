
[when]Chronos=$c : Chronos()
[when]There is a player=$player : Player()
[when]- ai=motivator == Motivator.AI
[when]- human=motivator == Motivator.HUMAN
[when]- has a turn=chronos < $c.getCurrent()

[then]log {message}=log.info({message});
