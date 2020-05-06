
[when]Chronos=$c : Chronos()
[when]There is a player=$player : Player()
[when]- ai=motivator == Motivator.AI
[when]- human=motivator == Motivator.HUMAN
[when]- has a turn=chronos < $c.getCurrent()

[then]log {message}=log.info({message});

[when]Story Goal "{name}"=$goal : GoalStatus(goal.name == "{name}", getFulfilled() == false)
[when]player enters room "{room_id}"=Player(room.modelId == "{room_id}")
[then]goal is fulfilled=engine.fulfill($goal)
