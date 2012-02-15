package models

object Phase extends Enumeration {
	type Phase = Value
	
	val MD1 = Value // Match Day 1
	val MD2 = Value // Match Day 2
	val MD3 = Value // Match Day 3
	val QUARTERFINALS = Value // Quarter Finals
	val SEMIFINALS = Value // Semi Finals
	val FINAL = Value // Final
}

object MatchOutcome extends Enumeration {
	type MatchOutcome = Value
	
	val DRAW = Value // no winner
	val TEAM_A = Value // first team wins
	val TEAM_B = Value // second team wins
}


