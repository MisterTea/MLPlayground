namespace java com.github.mistertea.herosiege
namespace scala com.github.mistertea.herosiege.scala

struct Stat {
	1:i32 current,
	2:i32 base,
	3:i32 add,
	4:i32 multiply,
	5:i32 divide,
}

struct Hero {
	1:string name,
	2:Stat health,
}
